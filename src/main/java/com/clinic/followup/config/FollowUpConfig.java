package com.clinic.followup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "followup")
public class FollowUpConfig {
    private Escalation escalation = new Escalation();
    private Reminder reminder = new Reminder();
    private Risk risk = new Risk();

    public Escalation getEscalation() {
        return escalation;
    }

    public void setEscalation(Escalation escalation) {
        this.escalation = escalation;
    }

    public Reminder getReminder() {
        return reminder;
    }

    public void setReminder(Reminder reminder) {
        this.reminder = reminder;
    }

    public Risk getRisk() {
        return risk;
    }

    public void setRisk(Risk risk) {
        this.risk = risk;
    }

    public static class Escalation {
        private int threshold = 3;

        public int getThreshold() {
            return threshold;
        }

        public void setThreshold(int threshold) {
            this.threshold = threshold;
        }
    }

    public static class Reminder {
        private int defaultDays = 7;

        public int getDefaultDays() {
            return defaultDays;
        }

        public void setDefaultDays(int defaultDays) {
            this.defaultDays = defaultDays;
        }
    }

    public static class Risk {
        private int highInterval = 3;
        private int mediumInterval = 7;
        private int lowInterval = 14;

        public int getHighInterval() {
            return highInterval;
        }

        public void setHighInterval(int highInterval) {
            this.highInterval = highInterval;
        }

        public int getMediumInterval() {
            return mediumInterval;
        }

        public void setMediumInterval(int mediumInterval) {
            this.mediumInterval = mediumInterval;
        }

        public int getLowInterval() {
            return lowInterval;
        }

        public void setLowInterval(int lowInterval) {
            this.lowInterval = lowInterval;
        }
    }
}
