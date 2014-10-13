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
