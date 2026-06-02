package com.intrackerone.domain.status;

import com.intrackerone.domain.history.StatusChangeRepository;
import com.intrackerone.domain.job.JobApplicationRepository;
import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import com.intrackerone.domain.user.UserRepository;
import com.intrackerone.infrastructure.exception.ResourceNotFoundException;
import com.intrackerone.infrastructure.exception.UnauthorizedException;
import com.intrackerone.infrastructure.security.AccessControl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PipelineStatusService {

    private final PipelineStatusRepository pipelineStatusRepository;
    private final PipelineStatusMapper pipelineStatusMapper;
    private final UserRepository userRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final StatusChangeRepository statusChangeRepository;

    @Transactional(readOnly = true)
    public List<PipelineStatusDto> findByOwnerId(Long ownerId) {
        return pipelineStatusRepository.findByOwnerIdOrderBySortOrderAsc(ownerId).stream()
                .map(pipelineStatusMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PipelineStatusDto> findDefaultStatuses() {
        return pipelineStatusRepository.findByIsDefaultTrueOrderBySortOrderAsc().stream()
                .map(pipelineStatusMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PipelineStatusDto findById(Long id, Long currentUserId, Role role) {
        PipelineStatus status = findStatus(id);
        AccessControl.requireOwnerOrGlobal(status.getOwner(), currentUserId, role, "PipelineStatus");
        return pipelineStatusMapper.toDto(status);
    }

    @Transactional
    public void seedDefaultsForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!pipelineStatusRepository.findByOwnerIdOrderBySortOrderAsc(userId).isEmpty()) {
            return;
        }
        seedDefaultStatusesIfNeeded();
        List<PipelineStatus> defaults = pipelineStatusRepository.findByIsDefaultTrueOrderBySortOrderAsc();
        List<PipelineStatus> copies = defaults.stream()
                .map(d -> PipelineStatus.builder()
                        .name(d.getName())
                        .sortOrder(d.getSortOrder())
                        .color(d.getColor())
                        .type(d.getType())
                        .isDefault(false)
                        .owner(user)
                        .build())
                .toList();
        pipelineStatusRepository.saveAll(copies);
        log.info("Seeded {} statuses for user {} from defaults", copies.size(), userId);
    }

    @Transactional
    public void seedDefaultStatusesIfNeeded() {
        if (!pipelineStatusRepository.findByIsDefaultTrueOrderBySortOrderAsc().isEmpty()) {
            return;
        }
        List<PipelineStatus> defaults = List.of(
            createDefaultStatus("Preparation", 1, "#9E9E9E", StatusType.ACTIVE),
            createDefaultStatus("Applied", 2, "#2196F3", StatusType.ACTIVE),
            createDefaultStatus("Phone Screen", 3, "#00BCD4", StatusType.ACTIVE),
            createDefaultStatus("Behavioral Interview", 4, "#4FC3F7", StatusType.ACTIVE),
            createDefaultStatus("DSA Interview", 5, "#FF5722", StatusType.ACTIVE),
            createDefaultStatus("PO Interview", 6, "#E040FB", StatusType.ACTIVE),
            createDefaultStatus("Technical Interview", 7, "#FF9800", StatusType.ACTIVE),
            createDefaultStatus("On-site / Final", 8, "#9C27B0", StatusType.ACTIVE),
            createDefaultStatus("Offer", 9, "#4CAF50", StatusType.ACTIVE),
            createDefaultStatus("Negotiation", 10, "#009688", StatusType.ACTIVE),
            createDefaultStatus("Accepted", 11, "#1B5E20", StatusType.TERMINAL),
            createDefaultStatus("Rejected", 12, "#F44336", StatusType.TERMINAL),
            createDefaultStatus("Withdrawn", 13, "#616161", StatusType.TERMINAL),
            createDefaultStatus("Ghosted", 14, "#795548", StatusType.TERMINAL)
        );
        pipelineStatusRepository.saveAll(defaults);
        log.info("Seeded {} default statuses", defaults.size());
    }

    private PipelineStatus createDefaultStatus(String name, int order, String color, StatusType type) {
        return PipelineStatus.builder()
                .name(name)
                .sortOrder(order)
                .color(color)
                .type(type)
                .isDefault(true)
                .owner(null)
                .build();
    }

    @Transactional
    public PipelineStatusDto create(PipelineStatusForm form, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        int maxOrder = pipelineStatusRepository.findByOwnerIdOrderBySortOrderAsc(userId).stream()
                .mapToInt(PipelineStatus::getSortOrder)
                .max()
                .orElse(0);

        PipelineStatus status = pipelineStatusMapper.toEntity(form);
        status.setSortOrder(maxOrder + 1);
        status.setOwner(user);
        status = pipelineStatusRepository.save(status);
        return pipelineStatusMapper.toDto(status);
    }

    @Transactional
    public PipelineStatusDto update(Long id, PipelineStatusForm form, Long currentUserId, Role role) {
        PipelineStatus status = findStatus(id);
        AccessControl.requireOwnerOrGlobal(status.getOwner(), currentUserId, role, "PipelineStatus");
        status.setName(form.getName());
        status.setColor(form.getColor());
        status.setType(form.getType());
        status = pipelineStatusRepository.save(status);
        return pipelineStatusMapper.toDto(status);
    }

    @Transactional
    public void delete(Long id, Long currentUserId, Role role) {
        PipelineStatus status = findStatus(id);
        AccessControl.requireOwnerOrGlobal(status.getOwner(), currentUserId, role, "PipelineStatus");
        if (!jobApplicationRepository.findByCurrentStatusId(id).isEmpty()) {
            throw new UnauthorizedException("Cannot delete status used by active job applications. Change job statuses first.");
        }
        if (!statusChangeRepository.findByToStatusId(id).isEmpty()) {
            throw new UnauthorizedException("Cannot delete status with recorded status changes.");
        }
        pipelineStatusRepository.delete(status);
    }

    @Transactional
    public void move(Long id, int newOrder, Long currentUserId, Role role) {
        PipelineStatus status = findStatus(id);
        AccessControl.requireOwnerOrGlobal(status.getOwner(), currentUserId, role, "PipelineStatus");
        Long ownerId = status.getOwner().getId();
        int oldOrder = status.getSortOrder();

        if (newOrder == oldOrder) {
            return;
        }

        List<PipelineStatus> all = pipelineStatusRepository.findByOwnerIdOrderBySortOrderAsc(ownerId);
        for (PipelineStatus s : all) {
            if (s.getId().equals(id)) {
                continue;
            }
            if (newOrder > oldOrder) {
                if (s.getSortOrder() > oldOrder && s.getSortOrder() <= newOrder) {
                    s.setSortOrder(s.getSortOrder() - 1);
                }
            } else {
                if (s.getSortOrder() >= newOrder && s.getSortOrder() < oldOrder) {
                    s.setSortOrder(s.getSortOrder() + 1);
                }
            }
        }
        pipelineStatusRepository.saveAll(all);

        status.setSortOrder(newOrder);
        pipelineStatusRepository.save(status);
    }

    @Transactional
    public PipelineStatusDto createDefault(PipelineStatusForm form) {
        int maxOrder = pipelineStatusRepository.findByIsDefaultTrueOrderBySortOrderAsc().stream()
                .mapToInt(PipelineStatus::getSortOrder)
                .max()
                .orElse(0);

        PipelineStatus status = pipelineStatusMapper.toEntity(form);
        status.setSortOrder(maxOrder + 1);
        status.setIsDefault(true);
        status.setOwner(null);
        status = pipelineStatusRepository.save(status);
        return pipelineStatusMapper.toDto(status);
    }

    @Transactional
    public PipelineStatusDto updateDefault(Long id, PipelineStatusForm form) {
        PipelineStatus status = findDefaultStatus(id);
        status.setName(form.getName());
        status.setColor(form.getColor());
        status.setType(form.getType());
        status = pipelineStatusRepository.save(status);
        return pipelineStatusMapper.toDto(status);
    }

    @Transactional
    public void deleteDefault(Long id) {
        PipelineStatus status = findDefaultStatus(id);
        pipelineStatusRepository.delete(status);
    }

    @Transactional
    public void moveDefault(Long id, int newOrder) {
        PipelineStatus status = findDefaultStatus(id);
        int oldOrder = status.getSortOrder();

        if (newOrder == oldOrder) {
            return;
        }

        List<PipelineStatus> all = pipelineStatusRepository.findByIsDefaultTrueOrderBySortOrderAsc();
        for (PipelineStatus s : all) {
            if (s.getId().equals(id)) {
                continue;
            }
            if (newOrder > oldOrder) {
                if (s.getSortOrder() > oldOrder && s.getSortOrder() <= newOrder) {
                    s.setSortOrder(s.getSortOrder() - 1);
                }
            } else {
                if (s.getSortOrder() >= newOrder && s.getSortOrder() < oldOrder) {
                    s.setSortOrder(s.getSortOrder() + 1);
                }
            }
        }
        pipelineStatusRepository.saveAll(all);

        status.setSortOrder(newOrder);
        pipelineStatusRepository.save(status);
    }

    private PipelineStatus findStatus(Long id) {
        return pipelineStatusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PipelineStatus", id));
    }

    private PipelineStatus findDefaultStatus(Long id) {
        PipelineStatus status = pipelineStatusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PipelineStatus", id));
        if (!Boolean.TRUE.equals(status.getIsDefault())) {
            throw new UnauthorizedException("Not a default status");
        }
        return status;
    }
}
