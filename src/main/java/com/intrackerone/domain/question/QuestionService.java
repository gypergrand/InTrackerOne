package com.intrackerone.domain.question;

import com.intrackerone.domain.recruiter.Recruiter;
import com.intrackerone.domain.recruiter.RecruiterRepository;
import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import com.intrackerone.domain.user.UserRepository;
import com.intrackerone.infrastructure.exception.ResourceNotFoundException;
import com.intrackerone.infrastructure.security.AccessControl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionMapper questionMapper;
    private final UserRepository userRepository;
    private final RecruiterRepository recruiterRepository;

    @Transactional(readOnly = true)
    public List<QuestionDto> findOwnQuestions(Long userId) {
        return questionRepository.findByOwnerIdAndIsDefaultFalse(userId).stream()
                .map(questionMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuestionDto> findDefaultQuestions() {
        return questionRepository.findByIsDefaultTrue().stream()
                .map(questionMapper::toDto)
                .toList();
    }

    @Transactional
    public QuestionDto create(QuestionForm form, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Question question = questionMapper.toEntity(form);
        question.setOwner(user);
        question.setIsDefault(false);
        resolveRecruiter(question, form.getRecruiterId(), user);
        question = questionRepository.save(question);
        log.info("Created question for user {}", userId);
        return questionMapper.toDto(question);
    }

    @Transactional
    public QuestionDto update(Long id, QuestionForm form, Long currentUserId, Role role) {
        Question question = findQuestion(id);
        AccessControl.requireOwnerOrGlobal(question.getOwner(), currentUserId, role, "Question");
        question.setQuestion(form.getQuestion());
        question.setAnswer(form.getAnswer());
        question.setNotes(form.getNotes());
        resolveRecruiter(question, form.getRecruiterId(), question.getOwner());
        question = questionRepository.save(question);
        return questionMapper.toDto(question);
    }

    @Transactional
    public void delete(Long id, Long currentUserId, Role role) {
        Question question = findQuestion(id);
        AccessControl.requireOwnerOrGlobal(question.getOwner(), currentUserId, role, "Question");
        questionRepository.delete(question);
    }

    @Transactional
    public QuestionDto addFromDefault(Long defaultId, Long userId) {
        Question defaultQuestion = questionRepository.findById(defaultId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", defaultId));
        if (!Boolean.TRUE.equals(defaultQuestion.getIsDefault())) {
            throw new IllegalArgumentException("Not a default question");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Question copy = Question.builder()
                .question(defaultQuestion.getQuestion())
                .answer(defaultQuestion.getAnswer())
                .notes(defaultQuestion.getNotes())
                .recruiter(null)
                .owner(user)
                .isDefault(false)
                .build();
        copy = questionRepository.save(copy);
        log.info("Added default question {} for user {}", defaultId, userId);
        return questionMapper.toDto(copy);
    }

    @Transactional
    public QuestionDto createDefault(QuestionForm form) {
        Question question = questionMapper.toEntity(form);
        question.setIsDefault(true);
        question.setOwner(null);
        question.setRecruiter(null);
        question = questionRepository.save(question);
        return questionMapper.toDto(question);
    }

    @Transactional
    public QuestionDto updateDefault(Long id, QuestionForm form) {
        Question question = findDefaultQuestion(id);
        question.setQuestion(form.getQuestion());
        question.setAnswer(form.getAnswer());
        question.setNotes(form.getNotes());
        question = questionRepository.save(question);
        return questionMapper.toDto(question);
    }

    @Transactional
    public void deleteDefault(Long id) {
        Question question = findDefaultQuestion(id);
        questionRepository.delete(question);
    }

    private Question findQuestion(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question", id));
    }

    private Question findDefaultQuestion(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question", id));
        if (!Boolean.TRUE.equals(question.getIsDefault())) {
            throw new IllegalArgumentException("Not a default question");
        }
        return question;
    }

    private void resolveRecruiter(Question question, Long recruiterId, User owner) {
        if (recruiterId == null) {
            question.setRecruiter(null);
            return;
        }
        Recruiter recruiter = recruiterRepository.findById(recruiterId)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter", recruiterId));
        AccessControl.requireSameOwner(owner, recruiter.getOwner(), "Recruiter");
        question.setRecruiter(recruiter);
    }
}
