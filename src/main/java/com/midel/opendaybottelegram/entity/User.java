package com.midel.opendaybottelegram.entity;

import com.midel.opendaybottelegram.entity.enums.State;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "username")
    private String username;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "speciality")
    private String speciality;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private State state;

    @Column(name = "hidden")
    private boolean hidden;

    @Column(name = "registered_at", columnDefinition = "TIMESTAMP WITH TIME ZONE", nullable = false)
    @CreationTimestamp
    private LocalDateTime registeredAt;
}
