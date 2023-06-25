package de.blutmondgilde.cloudsettings.api.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class BackendSessionTokenRequest {
    @Getter
    private String username, uuid, serverId;
}
