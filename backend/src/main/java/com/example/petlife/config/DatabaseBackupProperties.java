package com.example.petlife.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "db.backup")
public class DatabaseBackupProperties {

    private String pgDumpPath = "pg_dump";
    private String psqlPath = "psql";

    public String getPgDumpPath() { return pgDumpPath; }
    public void setPgDumpPath(String pgDumpPath) { this.pgDumpPath = pgDumpPath; }

    public String getPsqlPath() { return psqlPath; }
    public void setPsqlPath(String psqlPath) { this.psqlPath = psqlPath; }
}
