(ns sinai.entities-spec
  (:require [speclj.core]
            [sinai.entities :as e]
            clojure.set)
  (:require-macros [speclj.core :refer [describe it should= should should-not should-throw with]]))

(let [components {:component1 :some-data
                  :component2 :some-more-data}
      entity (e/create components)]
  (describe "create"
            (it "should create a new entity with the given components."
                (should (clojure.set/subset? (set components)
                                                 (set entity))))

            (it "should have an id."
                (should (e/get-id entity)))))

(let [entity1 (e/create {:component1 :some-data})
      entity2 (e/create {:component2 :some-data})
      entities (e/add-all {} [entity1 entity2])]
  (describe "a map of entities"
            (it "should contain all of the entities."
                (should= #{(e/get-id entity1) (e/get-id entity2)}
                         (set (e/get-all-ids entities))))

            (it "should allow entities to be looked up."
                (should= entity1
                         (e/get entities (e/get-id entity1)))
                (should= entity2
                         (e/get entities entity2)))

            (it "should allow entities to be updated."
                (should= :some-other-data
                         (-> entities
                             (e/update entity1 #(assoc % :component1 :some-other-data))
                             (e/get entity1)
                             :component1)))

            (it "should allow entities to be retreived by components."
                (should= #{(e/get-id entity1)}
                         (set (e/get-with entities #{:component1}))))))

(let [e1 (e/create {:position {:x 0 :y 0}})
      e2 (e/create {:position {:x 20 :y 20}})
      e3 (e/create {:position {:x -1 :y -1}})
      e4 (e/create {:position {:x 0 :y 0}
                    :hitbox {:width 15 :height 0}})]
  (describe "spatial indexing"
            (it "should add indexed entities"
                (should= {-1 {-1 #{(e/get-id e3)}}
                          0 {0 #{(e/get-id e1)
                                 (e/get-id e4)}}
                          1 {0 #{(e/get-id e4)}}
                          2 {2 #{(e/get-id e2)}}}
                         (-> (e/create-spatial-index 10)
                             (e/add-to-spatial-index e1)
                             (e/add-to-spatial-index e2)
                             (e/add-to-spatial-index e3)
                             (e/add-to-spatial-index e4)
                             :ids)))
            (it "should remove indexed entities"
                (should= {0 {0 #{(e/get-id e1)}}}
                         (-> (e/create-spatial-index 10)
                             (e/add-to-spatial-index e1)
                             (e/add-to-spatial-index e2)
                             (e/remove-from-spatial-index e2)
                             :ids)))))
