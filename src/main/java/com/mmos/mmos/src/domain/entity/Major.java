package com.mmos.mmos.src.domain.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.mmos.mmos.src.domain.dto.major.MajorSaveRequestDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class Major {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long majorIndex;

    @Column
    private String majorName;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "collegeIndex")
    private College college;

    @JsonManagedReference
    @OneToMany(mappedBy = "major", cascade = CascadeType.REMOVE,orphanRemoval = true)
    private List<User> majorUsers;

    public Major(MajorSaveRequestDto requestDto, College college) {
        this.majorName = requestDto.getName();
        this.college = college;
    }

    public void addUser(User user){
        this.majorUsers.add(user);
    }
}
