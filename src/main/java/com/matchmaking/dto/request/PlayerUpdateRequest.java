package com.matchmaking.dto.request;

import com.matchmaking.constants.Region;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PlayerUpdateRequest {

    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    private String username;

    @Email(message = "Email must be valid")
    private String email;

    private Region region;
}
