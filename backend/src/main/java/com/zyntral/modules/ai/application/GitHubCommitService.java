package com.zyntral.modules.ai.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches commit metadata from the GitHub REST API.
 * Uses public access by default (60 req/hour). Pass a personal access token
 * in the request for private repos or higher rate limits.
 */
@Service
public class GitHubCommitService {

    private static final Pattern REPO_PATTERN =
            Pattern.compile("github\\.com[/:]([^/]+)/([^/\\.]+?)(?:\\.git)?(?:[/?#].*)?$");

    public record CommitSummary(
            String sha,
            String shortSha,
            String message,
            String author,
            String date,
            int additions,
            int deletions,
            int filesChanged,
            List<String> changedFiles,
            String repoName,
            String repoOwner
    ) {}

    public CommitSummary fetch(String repoUrl, String sha, String githubToken) {
        String[] parsed = parseRepo(repoUrl);
        String owner = parsed[0];
        String repo  = parsed[1];
        String ref   = (sha == null || sha.isBlank()) ? "HEAD" : sha;

        RestClient client = buildClient(githubToken);

        CommitResponse response = client.get()
                .uri("/repos/{owner}/{repo}/commits/{ref}", owner, repo, ref)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new IllegalArgumentException(
                            "GitHub API error " + res.getStatusCode().value() +
                            ": check repo URL, commit SHA, and token permissions.");
                })
                .body(CommitResponse.class);

        if (response == null) throw new IllegalStateException("GitHub API returned no data");

        List<String> files = response.files() == null ? List.of() :
                response.files().stream().limit(15).map(CommitFile::filename).toList();

        int adds = response.files() == null ? 0 :
                response.files().stream().mapToInt(f -> f.additions).sum();
        int dels = response.files() == null ? 0 :
                response.files().stream().mapToInt(f -> f.deletions).sum();

        return new CommitSummary(
                response.sha(),
                response.sha().substring(0, Math.min(7, response.sha().length())),
                response.commit().message().lines().findFirst().orElse("").strip(),
                response.commit().author().name(),
                response.commit().author().date(),
                adds, dels, files.size(), files,
                repo, owner);
    }

    private RestClient buildClient(String token) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28");
        if (token != null && !token.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + token);
        }
        return builder.build();
    }

    private String[] parseRepo(String url) {
        if (url == null || url.isBlank()) throw new IllegalArgumentException("Repository URL must not be blank");
        Matcher m = REPO_PATTERN.matcher(url);
        if (!m.find()) throw new IllegalArgumentException("Invalid GitHub repository URL: " + url);
        return new String[]{m.group(1), m.group(2)};
    }

    // ---- Jackson response DTOs ------------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CommitResponse(String sha, CommitDetail commit, List<CommitFile> files) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CommitDetail(String message, CommitAuthor author) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CommitAuthor(String name, String date) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CommitFile(String filename, int additions, int deletions, String status) {}
}
