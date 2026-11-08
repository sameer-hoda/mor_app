# Food reference data

## `usda-seed.json` (generated, committed)

Built by `../scripts/build-usda-seed.mjs` from:

- U.S. Department of Agriculture, Agricultural Research Service.
  FoodData Central, SR Legacy (April 2018).
  https://fdc.nal.usda.gov/download-datasets
  **Licence: public domain (U.S. federal government work, CC0).**

Regenerate with:

```
node scripts/build-usda-seed.mjs <path-to-unzipped-SR-Legacy-CSVs>
```

The script keeps generic (non-branded, non-restaurant, non-fast-food) rows,
scales macros to the first household portion when a gram weight exists
(otherwise per 100 g), and tags a rough veg/egg/non-veg flag from the food
category (blank when uncertain). Curated home-style Indian dishes live in
`src/db.js` (`FOODS`) and always take precedence in search ranking.

## Deliberately NOT used

- IFCT 2017 (`ifct2017` npm mirror): the tarball ships
  AGPL-3.0-only, which would taint the app database, so it was rejected
  despite its excellent Indian coverage. If the NIN ever releases the tables
  under a permissive licence, this is the first source to revisit.
- Open Food Facts: ODbL share-alike; fine over its API, but embedding a
  subset would carry share-alike obligations. Revisit if we move to live API
  lookup instead of a seeded catalog.
