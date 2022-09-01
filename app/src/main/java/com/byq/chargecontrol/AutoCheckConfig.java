package com.byq.chargecontrol;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class AutoCheckConfig {
    @JsonProperty("dateRefreshDelay")
    public Integer dateRefreshDelay;
    @JsonProperty("stopTemp")
    public Integer stopTemp;
    @JsonProperty("reduceToTemp")
    public Integer reduceToTemp;
    @JsonProperty("checkCurrentMaxTime")
    public Integer checkCurrentMaxTime;
    @JsonProperty("fastChargeCurrent")
    public Integer fastChargeCurrent;
}
