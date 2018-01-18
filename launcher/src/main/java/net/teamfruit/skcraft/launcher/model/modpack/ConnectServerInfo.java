package net.teamfruit.skcraft.launcher.model.modpack;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Strings;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class ConnectServerInfo {
    private String serverHost;
    private int serverPort;

    @JsonIgnore
    public boolean isValid() {
        return !Strings.isNullOrEmpty(serverHost) && serverPort > 0 && serverPort < 65535;
    }
}
