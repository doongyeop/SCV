package com.scv.domain.user.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ExternalCreateGithubRepositoryRequestDTO {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;
}