package com.intrackerone.domain.job;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM JobApplication j WHERE j.id = :id")
    Optional<JobApplication> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT j.owner.id FROM JobApplication j WHERE j.id = :id")
    Optional<Long> findOwnerIdById(@Param("id") Long id);

    @Query("SELECT DISTINCT j FROM JobApplication j " +
           "LEFT JOIN FETCH j.project p " +
           "LEFT JOIN FETCH p.company " +
           "LEFT JOIN FETCH j.recruiter " +
           "LEFT JOIN FETCH j.currentStatus " +
           "LEFT JOIN FETCH j.owner")
    List<JobApplication> findAllWithRelations();

    @Query("SELECT DISTINCT j FROM JobApplication j " +
           "LEFT JOIN FETCH j.project p " +
           "LEFT JOIN FETCH p.company " +
           "LEFT JOIN FETCH j.recruiter " +
           "LEFT JOIN FETCH j.currentStatus " +
           "LEFT JOIN FETCH j.owner " +
           "WHERE j.owner.id = :ownerId")
    List<JobApplication> findByOwnerIdWithRelations(@Param("ownerId") Long ownerId);

    List<JobApplication> findByOwnerId(Long ownerId);

    List<JobApplication> findByProjectId(Long projectId);

    List<JobApplication> findByCurrentStatusId(Long statusId);

    List<JobApplication> findByOwnerIdAndCurrentStatusId(Long ownerId, Long statusId);

    long countByOwnerId(Long ownerId);

    long countByOwnerIdAndCurrentStatusId(Long ownerId, Long statusId);

    @Query("SELECT j.currentStatus.id, COUNT(j) FROM JobApplication j " +
           "WHERE j.owner.id = :ownerId GROUP BY j.currentStatus.id")
    List<Object[]> countByOwnerIdGroupByStatusId(@Param("ownerId") Long ownerId);
}
