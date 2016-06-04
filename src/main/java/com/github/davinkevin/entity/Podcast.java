package com.github.davinkevin.entity;


import lombok.*;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Builder
@Getter @Setter
@Accessors(chain = true)
@NoArgsConstructor @AllArgsConstructor
public class Podcast implements Serializable {

    public static final Podcast DEFAULT_PODCAST = new Podcast();

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "UUID")
    private UUID id;

    private String title;

    @Column(length = 65535)
    private String url;
    private String signature;
    private String type;
    private ZonedDateTime lastUpdate;


    @OneToMany(mappedBy = "podcast", fetch = FetchType.LAZY, cascade=CascadeType.ALL, orphanRemoval=true)
    @OrderBy("PUB_DATE DESC")
    @Fetch(FetchMode.SUBSELECT)
    private Set<Item> items = new HashSet<>();

    @OneToOne(fetch = FetchType.EAGER, cascade={CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, orphanRemoval=true)
    private Cover cover;

    @Column(length = 65535 )
    private String description;

    private Boolean hasToBeDeleted;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER) @Fetch(FetchMode.SUBSELECT)
    @JoinTable(name = "PODCAST_TAGS", joinColumns = @JoinColumn(name = "PODCASTS_ID"), inverseJoinColumns = @JoinColumn(name = "TAGS_ID"))
    private Set<Tag> tags = new HashSet<>();

    @Override
    public String toString() {
        return "Podcast{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", signature='" + signature + '\'' +
                ", type='" + type + '\'' +
                ", lastUpdate=" + lastUpdate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Podcast)) return false;

        Podcast podcast = (Podcast) o;

        return new EqualsBuilder()
                .append(id, podcast.id)
                .append(title, podcast.title)
                .append(url, podcast.url)
                .append(signature, podcast.signature)
                .append(lastUpdate, podcast.lastUpdate)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(title)
                .append(url)
                .append(signature)
                .append(lastUpdate)
                .toHashCode();
    }

    public Boolean contains(Item item) {
        return items.contains(item);
    }

    public Podcast add(Item item) {
        items.add(item.setPodcast(this));
        return this;
    }

    public Podcast lastUpdateToNow() {
        this.lastUpdate = ZonedDateTime.now();
        return this;
    }

    public interface PodcastListingView {}
    public interface PodcastDetailsView extends PodcastListingView{}

}
