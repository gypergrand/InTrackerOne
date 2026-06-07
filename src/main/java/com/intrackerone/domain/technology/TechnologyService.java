package com.intrackerone.domain.technology;

import com.intrackerone.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TechnologyService {

    private final TechnologyRepository technologyRepository;

    @Transactional(readOnly = true)
    public List<TechnologyDto> findAll() {
        return technologyRepository.findAll().stream()
                .map(t -> new TechnologyDto(t.getId(), t.getName()))
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> findAllNames() {
        return findAll().stream()
                .map(TechnologyDto::name)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<TechnologyDto> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return technologyRepository.findByNameIgnoreCase(name)
                .map(t -> new TechnologyDto(t.getId(), t.getName()));
    }

    @Transactional
    public String create(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Technology name must not be blank");
        }
        Technology technology = Technology.builder().name(name.trim()).build();
        return technologyRepository.save(technology).getName();
    }

    @Transactional
    public String update(Long id, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Technology name must not be blank");
        }
        Technology technology = technologyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technology", id));
        technology.setName(name.trim());
        return technologyRepository.save(technology).getName();
    }

    @Transactional
    public void delete(Long id) {
        Technology technology = technologyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technology", id));
        technologyRepository.delete(technology);
    }

    public Set<Technology> resolveTechnologies(Set<String> technologyNames) {
        if (technologyNames == null || technologyNames.isEmpty()) {
            return new HashSet<>();
        }
        return technologyNames.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(name -> technologyRepository.findByNameIgnoreCase(name.trim())
                        .orElseGet(() -> technologyRepository.save(Technology.builder().name(name.trim()).build())))
                .collect(Collectors.toSet());
    }

}
