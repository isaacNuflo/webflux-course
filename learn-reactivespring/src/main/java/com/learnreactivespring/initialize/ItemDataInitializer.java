package com.learnreactivespring.initialize;

import com.learnreactivespring.document.Item;
import com.learnreactivespring.document.ItemCapped;
import com.learnreactivespring.repository.ItemReactiveCappedRepository;
import com.learnreactivespring.repository.ItemReactiveRepository;
import com.mongodb.reactivestreams.client.MongoCollection;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
@Profile("!test")
public class ItemDataInitializer implements CommandLineRunner {

    @Autowired
    private ItemReactiveRepository itemReactiveRepository;

    @Autowired
    private ItemReactiveCappedRepository itemReactiveCappedRepository;

    @Autowired
    private ReactiveMongoOperations reactiveMongoOperations;

    @Override
    public void run(String... args) {
        initialDataSetUp();
        createCappedCollection();
        dataSetUpforCappedCollection();
    }

    @Transactional
    public void createCappedCollection() {
        reactiveMongoOperations.dropCollection(ItemCapped.class).then().block(); //Not for permanent data
        reactiveMongoOperations.createCollection(ItemCapped.class, CollectionOptions.empty().capped().size(50000).maxDocuments(20)).then().block();
    }

    public List<Item> data() {
        return Arrays.asList(new Item(null, "Samsung TV", 399.99),
                new Item(null, "LG TV", 329.99),
                new Item(null, "Apple TV", 349.99),
                new Item("ABC", "Beats HeadPhones", 19.99));
    }

    private void initialDataSetUp() {
        itemReactiveRepository.deleteAll()
                .thenMany(Flux.fromIterable(this.data()))
                .flatMap(itemReactiveRepository::save)
                .thenMany(itemReactiveRepository.findAll())
                .subscribe(item -> {
                    System.out.println("Item inserted from CommandLineRunner: " + item);
                });
    }

    private void dataSetUpforCappedCollection() {
        Flux<ItemCapped> itemCappedFlux = Flux.interval(Duration.ofSeconds(1))
                .map(i -> new ItemCapped(null, "Random Item " + i, (100.00 + i)));
        itemReactiveCappedRepository.insert(itemCappedFlux)
                .subscribe(itemCapped -> {
                    log.info("Inserted Item is " + itemCapped);
                });
    }
}
