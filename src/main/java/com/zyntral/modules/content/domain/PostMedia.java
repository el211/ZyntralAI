package com.zyntral.modules.content.domain;

import jakarta.persistence.*;

import java.util.UUID;

/** An image/video/gif attached to a post. */
@Entity
@Table(name = "post_media")
public class PostMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false)
    private String url;

    @Column(name = "media_type", nullable = false)
    private String mediaType;

    @Column(name = "alt_text")
    private String altText;

    @Column(nullable = false)
    private short position = 0;

    protected PostMedia() {}

    static PostMedia of(Post post, String url, String mediaType, String altText, short position) {
        PostMedia m = new PostMedia();
        m.post = post;
        m.url = url;
        m.mediaType = mediaType;
        m.altText = altText;
        m.position = position;
        return m;
    }

    public UUID getId() { return id; }
    public String getUrl() { return url; }
    public String getMediaType() { return mediaType; }
    public String getAltText() { return altText; }
    public short getPosition() { return position; }
}
