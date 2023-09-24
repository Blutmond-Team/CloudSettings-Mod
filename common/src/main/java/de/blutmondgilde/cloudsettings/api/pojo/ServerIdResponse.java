package de.blutmondgilde.cloudsettings.api.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ServerIdResponse {
    @Getter
    private String serverId;
}
