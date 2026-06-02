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
    public PipelineStatusDto findById(Long id, Long currentUserId, Role role) {
        PipelineStatus status = findStatus(id);
        AccessControl.requireOwnerOrGlobal(status.getOwner(), currentUserId, role, "PipelineStatus");
        return pipelineStatusMapper.toDto(status);
    }

    @Transactional
    public void seedDefaultsForUser(Long userId) {
        if (pipelineStatusRepository.existsByOwnerIdAndName(userId, "Preparation")) {
            return;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        List<PipelineStatus> defaults = defaultStatuses(user);
        pipelineStatusRepository.saveAll(defaults);
        log.info("Seeded {} default statuses for user {}", defaults.size(), userId);
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

        // Shift other statuses to make room, avoiding duplicate sort orders
        List<PipelineStatus> all = pipelineStatusRepository.findByOwnerIdOrderBySortOrderAsc(ownerId);
        for (PipelineStatus s : all) {
            if (s.getId().equals(id)) {
                continue;
            }
            if (newOrder > oldOrder) {
                // Moving down: shift items between (oldOrder, newOrder] up by 1
                if (s.getSortOrder() > oldOrder && s.getSortOrder() <= newOrder) {
                    s.setSortOrder(s.getSortOrder() - 1);
                }
            } else {
                // Moving up: shift items between [newOrder, oldOrder) down by 1
                if (s.getSortOrder() >= newOrder && s.getSortOrder() < oldOrder) {
                    s.setSortOrder(s.getSortOrder() + 1);
                }
            }
        }
        pipelineStatusRepository.saveAll(all);

        status.setSortOrder(newOrder);
        pipelineStatusRepository.save(status);
    }

    private List<PipelineStatus> defaultStatuses(User user) {
        return List.of(
            createStatus("Preparation", 1, "#9E9E9E", StatusType.ACTIVE, user),
            createStatus("Applied", 2, "#2196F3", StatusType.ACTIVE, user),
            createStatus("Phone Screen", 3, "#00BCD4", StatusType.ACTIVE, user),
            createStatus("Technical Interview", 4, "#FF9800", StatusType.ACTIVE, user),
            createStatus("On-site / Final", 5, "#9C27B0", StatusType.ACTIVE, user),
            createStatus("Offer", 6, "#4CAF50", StatusType.ACTIVE, user),
            createStatus("Negotiation", 7, "#009688", StatusType.ACTIVE, user),
            createStatus("Accepted", 8, "#1B5E20", StatusType.TERMINAL, user),
            createStatus("Rejected", 9, "#F44336", StatusType.TERMINAL, user),
            createStatus("Withdrawn", 10, "#616161", StatusType.TERMINAL, user),
            createStatus("Ghosted", 11, "#795548", StatusType.TERMINAL, user)
        );
    }

    private PipelineStatus createStatus(String name, int order, String color, StatusType type, User user) {
        return PipelineStatus.builder()
                .name(name)
                .sortOrder(order)
                .color(color)
                .type(type)
                .owner(user)
                .build();
    }

    private PipelineStatus findStatus(Long id) {
        return pipelineStatusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PipelineStatus", id));
    }
}
