package com.example.petlife.service;

import com.example.petlife.config.DatabaseBackupProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DatabaseBackupService {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    private final DatabaseBackupProperties backupProps;

    private static final Pattern JDBC_PATTERN =
            Pattern.compile("jdbc:postgresql://([^:/]+):(\\d+)/([^?]+)");

    private record DbConn(String host, int port, String dbName) {}

    public DatabaseBackupService(DatabaseBackupProperties backupProps) {
        this.backupProps = backupProps;
    }

    private DbConn parseJdbcUrl() {
        Matcher m = JDBC_PATTERN.matcher(jdbcUrl);
        if (!m.find()) {
            throw new IllegalStateException("JDBCのURL解析に失敗しました: " + jdbcUrl);
        }
        return new DbConn(m.group(1), Integer.parseInt(m.group(2)), m.group(3).trim());
    }

    public byte[] backup() throws Exception {
        DbConn conn = parseJdbcUrl();
        ProcessBuilder pb = new ProcessBuilder(
                backupProps.getPgDumpPath(),
                "-h", conn.host(),
                "-p", String.valueOf(conn.port()),
                "-U", dbUsername,
                "--no-password",
                conn.dbName()
        );
        pb.environment().put("PGPASSWORD", dbPassword);

        Process proc = pb.start();
        byte[] out = proc.getInputStream().readAllBytes();
        String err = new String(proc.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        boolean finished = proc.waitFor(5, TimeUnit.MINUTES);

        if (!finished) {
            proc.destroyForcibly();
            throw new RuntimeException("pg_dump がタイムアウトしました（5分）");
        }
        if (proc.exitValue() != 0) {
            throw new RuntimeException("pg_dump が失敗しました:\n" + err);
        }
        return out;
    }

    public void restore(byte[] sqlContent) throws Exception {
        DbConn conn = parseJdbcUrl();
        ProcessBuilder pb = new ProcessBuilder(
                backupProps.getPsqlPath(),
                "-h", conn.host(),
                "-p", String.valueOf(conn.port()),
                "-U", dbUsername,
                "-d", conn.dbName(),
                "--no-password"
        );
        pb.environment().put("PGPASSWORD", dbPassword);

        Process proc = pb.start();
        proc.getOutputStream().write(sqlContent);
        proc.getOutputStream().close();

        String err = new String(proc.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        boolean finished = proc.waitFor(10, TimeUnit.MINUTES);

        if (!finished) {
            proc.destroyForcibly();
            throw new RuntimeException("psql がタイムアウトしました（10分）");
        }
        if (proc.exitValue() != 0) {
            throw new RuntimeException("psql が失敗しました:\n" + err);
        }
    }
}
