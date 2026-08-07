package com.zyntral.modules.ai.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Fetches a GitHub repository's file tree and downloads relevant source files
 * based on a feature description. Used to provide code context to the AI when
 * generating a "feature showcase" post.
 */
@Service
public class GitHubRepoService {

    private static final Pattern REPO_PATTERN =
            Pattern.compile("github\\.com[/:]([^/]+)/([^/\\.]+?)(?:\\.git)?(?:[/?#].*)?$");

    /** Code file extensions we'll consider for analysis. */
    private static final Set<String> CODE_EXTENSIONS = Set.of(
            ".ts", ".tsx", ".js", ".jsx", ".java", ".py", ".go", ".rs",
            ".kt", ".swift", ".cs", ".cpp", ".c", ".rb", ".php", ".vue",
            ".svelte", ".sql", ".sh", ".yaml", ".yml", ".json", ".md"
    );

    /** Max files to fetch (keeps token count manageable). */
    private static final int MAX_FILES = 5;

    /** Max chars per file (avoids blowing up context). */
    private static final int MAX_FILE_CHARS = 3_000;

    public record FeatureCodeContext(
            String repoOwner,
            String repoName,
            String featureName,
            String featureDescription,
            List<SourceFile> files
    ) {}

    public record SourceFile(String path, String content) {}

    /**
     * Fetches the repo tree and downloads the files most relevant to the given feature.
     *
     * @param repoUrl          GitHub repository URL
     * @param featureName      Short name of the feature (e.g. "dark mode toggle")
     * @param featureDescription What the feature does / which files to look at
     * @param filePaths        Optional comma-separated list of specific file paths to include
     * @param githubToken      Optional PAT for private repos / higher rate limits
     */
    public FeatureCodeContext fetch(String repoUrl, String featureName,
                                   String featureDescription, String filePaths,
                                   String githubToken) {
        String[] parsed = parseRepo(repoUrl);
        String owner = parsed[0];
        String repo  = parsed[1];

        RestClient client = buildClient(githubToken);

        List<String> selectedPaths;

        if (filePaths != null && !filePaths.isBlank()) {
            // User specified exact files — respect them (up to MAX_FILES)
            selectedPaths = Arrays.stream(filePaths.split("[,\n]"))
                    .map(String::trim)
                    .filter(p -> !p.isEmpty())
                    .limit(MAX_FILES)
                    .toList();
        } else {
            // Auto-discover via tree + keyword matching
            selectedPaths = discoverRelevantFiles(client, owner, repo, featureName, featureDescription);
        }

        List<SourceFile> sourceFiles = selectedPaths.stream()
                .map(path -> fetchFile(client, owner, repo, path))
                .filter(Objects::nonNull)
                .toList();

        return new FeatureCodeContext(owner, repo, featureName, featureDescription, sourceFiles);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private List<String> discoverRelevantFiles(RestClient client, String owner, String repo,
                                               String featureName, String featureDescription) {
        // Fetch the recursive tree
        TreeResponse tree;
        try {
            tree = client.get()
                    .uri("/repos/{owner}/{repo}/git/trees/HEAD?recursive=1", owner, repo)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new IllegalArgumentException(
                                "GitHub API error " + res.getStatusCode().value() +
                                ": check repo URL and token permissions.");
                    })
                    .body(TreeResponse.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not fetch repository tree: " + ex.getMessage(), ex);
        }

        if (tree == null || tree.tree() == null) return List.of();

        // Extract keywords from feature name + description
        Set<String> keywords = extractKeywords(featureName + " " + featureDescription);

        // Score each file by how many keywords appear in its path
        List<TreeItem> codeFiles = tree.tree().stream()
                .filter(item -> "blob".equals(item.type()))
                .filter(item -> hasCodeExtension(item.path()))
                .filter(item -> !isBoilerplate(item.path()))
                .toList();

        return codeFiles.stream()
                .sorted(Comparator.comparingInt((TreeItem item) -> -scoreFile(item.path(), keywords)))
                .map(TreeItem::path)
                .limit(MAX_FILES)
                .collect(Collectors.toList());
    }

    private SourceFile fetchFile(RestClient client, String owner, String repo, String path) {
        try {
            FileResponse file = client.get()
                    .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, path)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new RuntimeException("Could not fetch " + path);
                    })
                    .body(FileResponse.class);

            if (file == null || file.content() == null) return null;

            // Content is base64-encoded with newlines
            String raw = new String(Base64.getMimeDecoder().decode(file.content()));
            if (raw.length() > MAX_FILE_CHARS) raw = raw.substring(0, MAX_FILE_CHARS) + "\n… [truncated]";
            return new SourceFile(path, raw);
        } catch (Exception ex) {
            // Silently skip unreadable files
            return null;
        }
    }

    private Set<String> extractKeywords(String text) {
        Set<String> stopWords = Set.of("the", "a", "an", "of", "in", "on", "at", "to", "for",
                "is", "are", "was", "were", "i", "my", "and", "or", "that", "this", "with",
                "it", "be", "as", "by", "so", "we", "he", "she", "they", "have", "has",
                "do", "did", "will", "can", "from", "not", "but", "if", "up");
        return Arrays.stream(text.toLowerCase().split("[^a-z0-9]+"))
                .filter(w -> w.length() >= 3)
                .filter(w -> !stopWords.contains(w))
                .collect(Collectors.toSet());
    }

    private int scoreFile(String path, Set<String> keywords) {
        String lower = path.toLowerCase();
        return (int) keywords.stream().filter(lower::contains).count();
    }

    private boolean hasCodeExtension(String path) {
        for (String ext : CODE_EXTENSIONS) {
            if (path.toLowerCase().endsWith(ext)) return true;
        }
        return false;
    }

    private boolean isBoilerplate(String path) {
        String lower = path.toLowerCase();
        return lower.contains("node_modules") || lower.contains(".min.") ||
               lower.contains("dist/") || lower.contains("build/") ||
               lower.contains("vendor/") || lower.contains("__pycache__") ||
               lower.contains(".gradle/") || lower.contains("target/") ||
               lower.endsWith("package-lock.json") || lower.endsWith("yarn.lock") ||
               lower.endsWith("pnpm-lock.yaml");
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

    // ── Jackson DTOs ──────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TreeResponse(String sha, List<TreeItem> tree, boolean truncated) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TreeItem(String path, String type, String sha, String url) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FileResponse(String name, String path, String content, String encoding) {}
}
