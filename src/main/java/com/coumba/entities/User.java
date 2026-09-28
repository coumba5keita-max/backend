package com.coumba.entities;

import com.coumba.entities.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "\"user\"")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "firstname", nullable = false)
    private String firstname;

    @Column(name = "lastname", nullable = false)
    private String lastname;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // --- Relations ---

    // ManyToMany avec Team (Table de jointure : team_user)
    @JsonIgnore
    @ManyToMany(mappedBy = "users")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Team> teams = new HashSet<>();

    // ManyToMany avec Project (Table de jointure : project_user)
    @JsonIgnore
    @ManyToMany(mappedBy = "users")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Project> projects = new HashSet<>();

    // Relation inverse : Tâches créées par l'utilisateur
    @JsonIgnore
    @OneToMany(mappedBy = "creator")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Task> createdTasks = new HashSet<>();

    // Relation inverse : Tâches assignées à l'utilisateur
    @JsonIgnore
    @ManyToMany(mappedBy = "assignees")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Task> assignedTasks = new HashSet<>();

    // Relation inverse : Commentaires de l'utilisateur
    @JsonIgnore
    @OneToMany(mappedBy = "user")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Comment> comments = new HashSet<>();

    // Relation inverse : Fichiers joints uploadés par l'utilisateur
    @JsonIgnore
    @OneToMany(mappedBy = "user")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Attachment> attachments = new HashSet<>();

    // Relation inverse : Notifications destinées à l'utilisateur
    @JsonIgnore
    @OneToMany(mappedBy = "user")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Notification> notifications = new HashSet<>();
}
