package com.learnreactivespring.repository;

import com.learnreactivespring.document.Item;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@DataMongoTest
@RunWith(SpringRunner.class)
@DirtiesContext
public class ItemReactiveRepositoryTest {
    @Autowired
    private ItemReactiveRepository itemReactiveRepository;

    List<Item> itemList = Arrays.asList(new Item(null, "Samsung TV", 400.0),
            new Item(null, "LG TV", 420.0),
            new Item(null, "Apple Watch", 299.0),
            new Item(null, "Beats Headphones", 149.0),
            new Item("ASD", "Bose Headphones", 149.0));

    @Before
    public void setUp() {
        itemReactiveRepository.deleteAll()
                .thenMany(Flux.fromIterable(itemList))
                .flatMap(itemReactiveRepository::save)
                .doOnNext(item -> System.out.println("Inserted Item is: " + item))
                .blockLast();
    }

    @Test
    public void getAllItems() {
        Flux<Item> items = itemReactiveRepository.findAll();

        StepVerifier.create(items)
                .expectSubscription()
                .expectNextCount(5L)
                .verifyComplete();
    }

    @Test
    public void getItemById() {
        Mono<Item> item = itemReactiveRepository.findById("ASD");
        StepVerifier.create(item)
                .expectSubscription()
                .expectNextMatches(item1 -> item1.getDescription().equals("Bose Headphones"))
                .verifyComplete();
    }

    @Test
    public void getByDescription(){
        Mono<Item> items = itemReactiveRepository.findByDescription("LG TV");

        StepVerifier.create(items)
                .expectSubscription()
                .expectNextCount(1L)
                .verifyComplete();
    }

    @Test
    public void insertItem(){
        Item item = new Item(null, "SkullCandy Headphones", 120.0);
        StepVerifier.create(itemReactiveRepository.save(item).log())
                .expectSubscription()
                .expectNextMatches(item1 -> Objects.nonNull(item1.getId()) && item1.getDescription().equals("SkullCandy Headphones"))
                .verifyComplete();
    }

    @Test
    public void updateItem(){
        double newPrice = 520.0;
        Mono<Item> item = itemReactiveRepository.findById("ASD")
                .map(item1 -> {
                    item1.setPrice(newPrice);
                    return item1;
                })
                .flatMap(itemReactiveRepository::save);
        StepVerifier.create(item)
                .expectSubscription()
                .expectNextMatches(itemUpdated -> itemUpdated.getPrice().equals(520.0))
                .verifyComplete();
    }

    @Test
    public void deleteItem() {
        Mono<Void> deletedItem = itemReactiveRepository.findById("ASD")
                .map(Item::getId)
                .flatMap(itemReactiveRepository::deleteById);
        StepVerifier.create(deletedItem)
                .expectSubscription()
                .verifyComplete();

        StepVerifier.create(itemReactiveRepository.findAll())
                .expectSubscription()
                .expectNextCount(4L)
                .verifyComplete();
    }
}
