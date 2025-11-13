package com.i2i.user_management.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Data
@SuperBuilder
@NoArgsConstructor
@Entity
public class Role extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    private String name;

    private String description;

    private Boolean isDeleted = false;

}

