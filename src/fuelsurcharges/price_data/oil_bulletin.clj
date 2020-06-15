(ns fuelsurcharges.price-data.oil-bulletin
  (:require [clj-http.client :as http-client]
            [clojure.java.io]
            [clojure.tools.logging :as log]
            [java-time :as jt]
            [oz.core :as oz]
            [fuelsurcharges.db.core :as db])
  (:import
   (org.apache.poi.ss.usermodel Cell Row Sheet Workbook WorkbookFactory))
  (:gen-class))

(defn load-workbook
  "Load a workbook from a string path."
  [path]
  (log/debugf "Loading workbook:" path)
  (doto (WorkbookFactory/create (clojure.java.io/input-stream path))
    (.setMissingCellPolicy Row/CREATE_NULL_AS_BLANK)))

(defn get-cell-string-value
  "Get the value of a cell as a string, by changing the cell type to 'string'
   and then changing it back."
  [cell]
  (let [ct    (.getCellType cell)
        _     (.setCellType cell Cell/CELL_TYPE_STRING)
        value (.getStringCellValue cell)]
    (.setCellType cell ct)
    value))

(defn read-row
  "Read all the cells in a sheet row (including blanks) and return a list of values."
  [row]
  (for [i (range 0 (.getLastCellNum row))]
    (get-cell-string-value (.getCell row (.intValue i)))))

(defn get-rows-from-sheet
  "Iterates through the sheet, returning a list of lists"
  [sheet]
  (->> sheet
       (.iterator)
       iterator-seq
       (map read-row)))

(defn filter-with-indices [indices coll]
  "Filters a collection by its indices."
  (keep-indexed #(when ((set indices) %1) %2) coll))

(defn indices [pred coll]
  "Returns the indices of a collection that match a predicate"
  (keep-indexed #(when (pred %2) %1) coll))

(clojure.java.io/copy
  (:body (http-client/get "http://ec.europa.eu/energy/observatory/reports/Oil_Bulletin_Prices_History.xlsx" {:as :stream}))
  (java.io.File. "downloads/Oil_Bulletin_Prices_History.xlsx"))

(def sheet-name "Prices with taxes, EU")

(def prices-sheet (let [workbook (load-workbook "downloads/Oil_Bulletin_Prices_History.xlsx")
                        sheet    (.getSheet workbook sheet-name)]
                    sheet))

(def raw-rows (get-rows-from-sheet prices-sheet))

(def rows (->> raw-rows
               (drop 5) ;; drop the first 5 rows
               (filter not-empty) ;; drop empty rows
               (map #(filter-with-indices '(1 3) %)) ;; take only the second and fourth columns
               (filter #(re-find #"\d{5}" (first %)))))

(frequencies (map count rows)) ;; check all rows have two elements in them

(defn local-date-from-excel [date-number]
  (jt/plus (jt/local-date 1900) (jt/days date-number)))

;; convert rows of strings into a list of maps
(def oil-price-data
  (->> rows
       (map (fn [[date price]] {:date  (->  date
                                            read-string
                                            local-date-from-excel
                                            jt/format)
                                :price (-> price
                                           read-string)}))
       (sort-by :date)))

(comment (take-last 5 oil-price-data))

;; convert rows into form needed by database
(defn market-prices-insert [prices]
  (for [{:keys [date price]} prices]
    [1 (jt/local-date date) price "EUR"]))

(comment (db/get-markets))
(comment (db/insert-market-prices! {:market-prices (market-prices-insert oil-price-data)}))
(comment (db/get-market-prices))
;; OZ
(comment (oz/start-server!))

(def line-plot
  {:title    "Price of automotive gas oil, 1000L"
   :data     {:values oil-price-data}
   :encoding {:x {:field "date" :type "ordinal"}
              :y {:field "price" :type "quantitative"}}
   :mark     "line"
   :width    800})

;; Render the plot
(comment (oz/view! line-plot))
