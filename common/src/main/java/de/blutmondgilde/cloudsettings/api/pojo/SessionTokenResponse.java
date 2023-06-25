package de.blutmondgilde.cloudsettings.api.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class SessionTokenResponse {
    @Getter
    private String token;
}
