package com.intrackerone.domain.project;

import com.intrackerone.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndustryServiceTest {

    @Mock
    private IndustryRepository industryRepository;

    @InjectMocks
    private IndustryService industryService;

    @Captor
    private ArgumentCaptor<Industry> industryCaptor;

    private Industry software;
    private Industry hardware;

    @BeforeEach
    void setUp() {
        software = Industry.builder().id(1L).name("Software Development").build();
        hardware = Industry.builder().id(2L).name("Hardware Engineering").build();
    }

    @Test
    void findAll_returnsAllSortedByName() {
        when(industryRepository.findAll()).thenReturn(List.of(hardware, software));

        List<IndustryDto> result = industryService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Hardware Engineering");
        assertThat(result.get(1).name()).isEqualTo("Software Development");
    }

    @Test
    void findAllNames_returnsSortedNames() {
        when(industryRepository.findAll()).thenReturn(List.of(software, hardware));

        List<String> result = industryService.findAllNames();

        assertThat(result).containsExactly("Hardware Engineering", "Software Development");
    }

    @Test
    void findByName_whenExists_returnsDto() {
        when(industryRepository.findByNameIgnoreCase("software development")).thenReturn(Optional.of(software));

        IndustryDto result = industryService.findByName("software development");

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Software Development");
    }

    @Test
    void findByName_whenNotExists_returnsNull() {
        when(industryRepository.findByNameIgnoreCase("unknown")).thenReturn(Optional.empty());

        IndustryDto result = industryService.findByName("unknown");

        assertThat(result).isNull();
    }

    @Test
    void findByName_whenNull_returnsNull() {
        IndustryDto result = industryService.findByName(null);

        assertThat(result).isNull();
        verifyNoInteractions(industryRepository);
    }

    @Test
    void create_savesAndReturnsName() {
        when(industryRepository.save(any())).thenReturn(software);

        String result = industryService.create("Software Development");

        assertThat(result).isEqualTo("Software Development");
        verify(industryRepository).save(industryCaptor.capture());
        assertThat(industryCaptor.getValue().getName()).isEqualTo("Software Development");
    }

    @Test
    void create_withBlankName_throwsException() {
        assertThatThrownBy(() -> industryService.create("  "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be blank");
        verifyNoInteractions(industryRepository);
    }

    @Test
    void create_withNullName_throwsException() {
        assertThatThrownBy(() -> industryService.create(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be blank");
        verifyNoInteractions(industryRepository);
    }

    @Test
    void create_trimsName() {
        Industry trimmed = Industry.builder().id(1L).name("Cloud Computing").build();
        when(industryRepository.save(any())).thenReturn(trimmed);

        industryService.create("  Cloud Computing  ");

        verify(industryRepository).save(industryCaptor.capture());
        assertThat(industryCaptor.getValue().getName()).isEqualTo("Cloud Computing");
    }

    @Test
    void update_modifiesName() {
        when(industryRepository.findById(1L)).thenReturn(Optional.of(software));
        when(industryRepository.save(any())).thenReturn(software);

        String result = industryService.update(1L, "AI & Machine Learning");

        assertThat(result).isEqualTo("AI & Machine Learning");
        verify(industryRepository).save(industryCaptor.capture());
        assertThat(industryCaptor.getValue().getName()).isEqualTo("AI & Machine Learning");
    }

    @Test
    void update_whenNotFound_throwsException() {
        when(industryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> industryService.update(99L, "New Name"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_withBlankName_throwsException() {
        assertThatThrownBy(() -> industryService.update(1L, "  "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be blank");
        verify(industryRepository, never()).findById(any());
    }

    @Test
    void delete_whenExists_deletes() {
        when(industryRepository.findById(1L)).thenReturn(Optional.of(software));

        industryService.delete(1L);

        verify(industryRepository).delete(software);
    }

    @Test
    void delete_whenNotFound_throwsException() {
        when(industryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> industryService.delete(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
