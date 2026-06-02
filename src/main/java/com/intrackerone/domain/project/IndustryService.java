package com.intrackerone.domain.project;

import com.intrackerone.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IndustryService {

    private final IndustryRepository industryRepository;

    @Transactional(readOnly = true)
    public List<IndustryDto> findAll() {
        return industryRepository.findAll().stream()
                .map(i -> new IndustryDto(i.getId(), i.getName()))
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> findAllNames() {
        return findAll().stream()
                .map(IndustryDto::name)
                .toList();
    }

    @Transactional(readOnly = true)
    public IndustryDto findByName(String name) {
        if (name == null) return null;
        return industryRepository.findByNameIgnoreCase(name)
                .map(i -> new IndustryDto(i.getId(), i.getName()))
                .orElse(null);
    }

    @Transactional
    public String create(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Industry name must not be blank");
        }
        Industry industry = Industry.builder().name(name.trim()).build();
        return industryRepository.save(industry).getName();
    }

    @Transactional
    public String update(Long id, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Industry name must not be blank");
        }
        Industry industry = industryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Industry", id));
        industry.setName(name.trim());
        return industryRepository.save(industry).getName();
    }

    @Transactional
    public void delete(Long id) {
        Industry industry = industryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Industry", id));
        industryRepository.delete(industry);
    }
}
