package com.github.davinkevin;

import com.github.davinkevin.entity.Cover;
import com.github.davinkevin.entity.Item;
import com.github.davinkevin.entity.Podcast;
import com.github.davinkevin.entity.Status;
import com.github.davinkevin.repository.ItemRepository;
import com.github.davinkevin.repository.PodcastRepository;
import io.codearte.jfairy.Fairy;
import io.codearte.jfairy.producer.BaseProducer;
import io.codearte.jfairy.producer.text.TextProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.IntStream;

@SpringBootApplication
public class IssueH2databaseApplication {

	public static void main(String[] args) {
		SpringApplication.run(IssueH2databaseApplication.class, args);
	}

	@Slf4j
	@Service
	@RequiredArgsConstructor(onConstructor = @__(@Autowired))
	public static class BatchInsert implements CommandLineRunner {

		private final PodcastRepository podcastRepository;
		private final ItemRepository itemRepository;
		/*private final CoverRepository coverRepository;*/

		/*@Override*/
		public void run(String... strings) throws Exception {
			Item.rootFolder = Paths.get("/tmp");
			log.info("Before :: Number of Podcast " + podcastRepository.findAll().size());
			log.info("Before :: Number of Item " + itemRepository.findAll().size());

			log.info("Beginning of force insert");
			List<Podcast> podcasts = generatePodcast(200);
			List<Item> items = generateItemsIn(50_000, podcasts);

			log.info("Number of podcast inserted : " + podcasts.size());
			log.info("Number of item inserted : " + items.size());

			log.info("Deletion");
			items.forEach(itemRepository::delete);
			podcasts.forEach(podcastRepository::delete);
			log.info("Deletion done");

			log.info("After :: Number of Podcast " + podcastRepository.findAll().size());
			log.info("After :: Number of Item " + itemRepository.findAll().size());
		}

		private List<Item> generateItemsIn(int numberOfItem, List<Podcast> podcasts) {
			log.info("Insert of Items");

			Fairy fairy = Fairy.create();
			TextProducer tp = fairy.textProducer();
			BaseProducer bp = fairy.baseProducer();
			ZonedDateTime now = ZonedDateTime.now();
			Integer numberOfPodcast = podcasts.size();


			IntStream
					.range(0, numberOfItem)
					.mapToObj(i -> Item.builder()
							.title(tp.randomString(6))
							.description(tp.paragraph(3))
							.length(bp.randomBetween(1000L, 100_000L))
							.creationDate(now.minusDays(i))
							.downloadDate(now.minusDays(i))
							.pubDate(now.minusDays(i))
							.fileName(tp.latinWord(1))
							.podcast(podcasts.get(bp.randomInt(numberOfPodcast-1)))
							.cover(generateCover(bp, tp, i))
							.status(Status.NOT_DOWNLOADED)
							.mimeType("video/foo")
							.url(generateUrl(tp, i))
						.build())
					.forEach(itemRepository::save);


			return itemRepository.findAll();
		}

		private List<Podcast> generatePodcast(int numberOfPodcast) {
			log.info("Insert of Podcast");
			Fairy fairy = Fairy.create();
			TextProducer tp = fairy.textProducer();
			BaseProducer bp = fairy.baseProducer();
			ZonedDateTime now = ZonedDateTime.now();

			IntStream
					.range(0, numberOfPodcast)
					.mapToObj(i -> Podcast.builder()
                            .description(tp.paragraph(4))
                            .title(tp.randomString(2))
							.url(generateUrl(tp, i))
                            .hasToBeDeleted(bp.trueOrFalse())
                            .lastUpdate(now.minusDays(i))
							.cover(generateCover(bp, tp, i))
							.build())
					.forEach(podcastRepository::saveAndFlush);

			return podcastRepository.findAll();
		}

		private String generateUrl(TextProducer tp, int i) {
			return "http://www."+tp.latinWord(1)+".com/"+ tp.latinWord(1) + "/" + i + ".mp4";
		}

		private Cover generateCover(BaseProducer baseProducer, TextProducer textProducer, int i) {
			return Cover.builder()
                    .height(baseProducer.randomBetween(0, 1400))
                    .width(baseProducer.randomBetween(0, 1400))
                    .url("http://"+textProducer.latinWord(1)+"/"+ textProducer.latinWord(1) + "." + i + ".png")
                    .build();
		}

	}
}
