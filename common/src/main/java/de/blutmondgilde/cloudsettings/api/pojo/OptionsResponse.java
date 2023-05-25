package de.blutmondgilde.cloudsettings.api.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class OptionsResponse {
    @Getter
    private String[] options;
}
