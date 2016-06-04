package com.github.davinkevin.entity;

import com.google.common.collect.Sets;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;


@Entity
@Slf4j
@Builder
@Getter @Setter
@Table(name = "item", uniqueConstraints = @UniqueConstraint(columnNames={"podcast_id", "url"}))
@Accessors(chain = true)
@NoArgsConstructor @AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Item {

    public  static Path rootFolder;
    public  static final Item DEFAULT_ITEM = new Item();
    private static final String PROXY_URL = "/api/podcast/%s/items/%s/download%s";

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "UUID")
    private UUID id;

    @OneToOne(fetch = FetchType.EAGER, cascade=CascadeType.ALL, orphanRemoval=true)
    private Cover cover;

    @ManyToOne(cascade={CascadeType.MERGE}, fetch = FetchType.EAGER)
    private Podcast podcast;
    private String title;

    @Column(length = 65535)
    private String url;
    private ZonedDateTime pubDate;

    @Column(length = 2147483647)
    private String description;

    private String mimeType;
    private Long length;
    private String fileName;

    /* Value for the Download */
    @Enumerated(EnumType.STRING)
    private Status status = Status.NOT_DOWNLOADED;

    @Transient
    private Integer progression = 0;

    @Transient
    private Integer numberOfTry = 0;

    private ZonedDateTime downloadDate;

    @CreatedDate
    private ZonedDateTime creationDate;

    @ManyToMany(mappedBy = "items", cascade = CascadeType.REFRESH)
    private Set<WatchList> watchLists = Sets.newHashSet();

    public String getLocalUri() {
        return (fileName == null) ? null : getLocalPath().toString();
    }

    public Item setLocalUri(String localUri) {
        fileName = FilenameUtils.getName(localUri);
        return this;
    }

    public Item addATry() {
        this.numberOfTry++;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item)) return false;
        if (this == DEFAULT_ITEM && o != DEFAULT_ITEM || this != DEFAULT_ITEM && o == DEFAULT_ITEM) return false;

        Item item = (Item) o;

        if (nonNull(id) && nonNull(item.id))
            return id.equals(item.id);

        if (nonNull(url) && nonNull(item.url)) {
            return url.equals(item.url) || FilenameUtils.getName(item.url).equals(FilenameUtils.getName(url));
        }

        return StringUtils.equals(getProxyURL(), item.getProxyURL());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(url)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "Item{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", pubDate=" + pubDate +
                ", description='" + description + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", length=" + length +
                ", status='" + status + '\'' +
                ", progression=" + progression +
                ", downloaddate=" + downloadDate +
                ", podcast=" + podcast +
                ", numberOfTry=" + numberOfTry +
                '}';
    }

    @Transient
    public String getProxyURL() {
        return String.format(PROXY_URL, podcast.getId(), id, getExtention());
    }

    @Transient
    public Boolean isDownloaded() {
        return StringUtils.isNotEmpty(fileName);
    }

    //* CallBack Method JPA *//
    @PreRemove
    public void preRemove() {
        checkAndDelete();
        watchLists.forEach(watchList -> watchList.remove(this));
    }

    private void checkAndDelete() {
        if (podcast.getHasToBeDeleted() && isDownloaded()) {
            deleteFile();
        }
    }

    private void deleteFile() {
        try {
            Files.deleteIfExists(getLocalPath());
        } catch (IOException e) {
            log.error("Error during deletion of {}", this, e);
        }
    }

    @Transient
    public Item deleteDownloadedFile() {
        deleteFile();
        status = Status.DELETED;
        fileName = null;
        return this;
    }

    public Path getLocalPath() {
        return rootFolder.resolve(podcast.getTitle()).resolve(fileName);
    }

    public String getProxyURLWithoutExtention() {
        return String.format(PROXY_URL, podcast.getId(), id, "");
    }

    private String getExtention() {
        String ext = FilenameUtils.getExtension(fileName);
        return (ext == null) ? "" : "."+ext;
    }

    public Cover getCoverOfItemOrPodcast() {
        return isNull(this.cover) ? podcast.getCover() : this.cover;
    }
    public UUID getPodcastId() { return isNull(podcast) ? null : podcast.getId();}
    public boolean hasValidURL() {
        return (!StringUtils.isEmpty(this.url)) || "send".equals(this.podcast.getType());
    }

    public Item reset() {
        checkAndDelete();
        setStatus(Status.NOT_DOWNLOADED);
        downloadDate = null;
        fileName = null;
        return this;
    }
}
