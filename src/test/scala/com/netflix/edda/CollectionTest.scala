package com.netflix.edda

import org.slf4j.{ Logger, LoggerFactory }

import com.netflix.edda.basic.BasicContext

import org.scalatest.FunSuite

class CollectionTest extends FunSuite {
    val logger = LoggerFactory.getLogger(getClass)
    test("load") {
        val coll = new TestCollection
        coll.start
        expect(Nil) {
            coll.query(Map("id" -> "b"))
        }
        
        coll.datastore.get.records = Seq(Record("a", 1), Record("b", 2), Record("c", 3))
        coll ! Collection.Load(coll)
        // allow for collection to load
        Thread.sleep(1000)
        val records = coll.query(Map("id" -> "b"))
        expect(1) {
            records.size
        }
        expect(2) {
            records.head.data
        }
        expect("b") {
            records.head.id
        }
        coll.stop
    }

    test("update") {
        val coll = new TestCollection
        coll.datastore.get.records = Seq(Record("a", 1), Record("b", 2), Record("c", 3))
        coll.start

        expect(3) {
            coll.query().size
        }

        coll.crawler.records = Seq(Record("a", 1), Record("b", 3), Record("c", 4), Record("d", 5))
        coll.crawler.crawl()
        // allow for crawl to propagate
        Thread.sleep(1000)
        
        val records = coll.query(Map("data" -> Map("$gte" -> 3)))
        
        expect(3) {
            records.size
        }
    }

    test("leader") {
        // check for election results every 100ms
        BasicContext.config.setProperty("edda.elector.refresh", "200")
        // collection should crawl every 100ms
        BasicContext.config.setProperty("edda.collection.refresh", "200")
        BasicContext.config.setProperty("edda.collection.cache.refresh", "200")
        val coll = new TestCollection
        val datastoreResults =  Seq(Record("a", 1), Record("b", 2), Record("c", 3))
        val crawlResults = Seq(Record("a", 1), Record("b", 3), Record("c", 4), Record("d", 5))

        coll.datastore.get.records = datastoreResults
        coll.start
        
        // expect data loaded form datastore
        expect(3) {
            coll.query().size
        }

        // set crawler results and wait for the crawler results to propagete
        coll.crawler.records = crawlResults
        Thread.sleep(1000)
        
        // we should get 4 records now
        expect(4) {
            coll.query().size
        }
        
        // now drop leader role and wait for datastore results to reload
        coll.elector.leader = false
        coll.datastore.get.records = datastoreResults
        Thread.sleep(1000)

        expect(3) {
            coll.query().size
        }

    }

    // test("query") {
    // }
}