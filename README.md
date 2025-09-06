1. clone the repositry 
2. git clone https://github.com/usersandeep/EA_food_automation_testing.git
3. open this project in eclipse 
4. you will get the project architecure

# Server
- open 'server' folder in command prompt and navigate inside to this project.
- then run the following command
> npm i
- after installing, then run the following command
> npm run server

- now the server will start

`Note: To run this server we need to install nodejs in our system.`

# after server start
1. navigate to src main java in eclipse
2. navigate to orders and products package
3. execute the test cases

 # QA Deliverables
1. Test Cases Document
2. Test Strategy Document
3. Bug Evidence Document
4. Postman Collection 


#Setup & Run

# Prerequisites
- Node.js (tested on v20+)
- npm

# Steps
```bash
# clone the repository
git clone https://github.com/usersandeep/EA_food_automation_testing.git
cd EA_food_automation_testing/server

# install dependencies
npm install

# start server
node server.js
Server starts at:
 http://localhost:5000

# API Overview

1. GET /products → list products from db.json

2. POST /orders → create new order, validates cutoff & stock, decrements stock

3. DELETE /orders/:id → cancel order, restores stock

4. POST /ops/update-stock → bulk update stock levels

# Business rules / assumptions

1. Cutoff: Orders placed strictly after 18:01 (6:01 PM), or with slot = "evening", add +1 extra day to delivery date.
2. Base delivery = requestedDate + 1 day.

3. Cancel: Cancelling an order restores the product stock count.

4. Data store: File-based db.json (5 products, 2 sample users).

5. Status: Field intentionally kept as "delievred" (per assignment instruction).

# Automated Tests

1. Two must-have automated test cases are implemented & verified:

2. Order at 6:01 PM goes to +2 days

3. Proof: see artifacts/tests/order_cutoff_test_output.txt

4. Screenshot: artifacts/screenshots/order_cutoff_test.png

5. Cancel order restores stock count

6. Proof: see artifacts/tests/cancel_restore_output.txt

7. Screenshot: artifacts/screenshots/cancel_restores_stock.png

8. Both were also run manually in Postman, with screenshots included in artifacts.

# Postman

1. Exported artifacts:

2. artifacts/postman/EA_Foods.postman_collection.json

3. artifacts/postman/EA_Foods.postman_environment.json

#How to use:

1. Import collection & environment in Postman.

2. Select environment EA_Foods_Local (baseUrl = http://localhost:5000
).

3. Run requests:

4. GET {{baseUrl}}/products

5. POST {{baseUrl}}/orders

6. DELETE {{baseUrl}}/orders/:id

7. POST {{baseUrl}}/ops/update-stock

# Artifacts

1. All proof files live under the /artifacts folder:

2. postman/ → collection & environment JSON

3. screenshots/ → screenshots & Word doc with test evidence

4. tests/ → test scripts & outputs

5. bug_reports.md → list of identified bugs

# Bug Reports 

1. Status spelling typo: "delievred" instead of "delivered".

2. Slot validation missing: invalid slot values accepted.

3. Parallel race condition: two concurrent orders can make stock negative.

4. (Full details in artifacts/bug_reports.md.)

# Time Log

1. Setup & server customization: ~1.0 hr

2. Writing & validating tests: ~0.5 hr

3.Documentation & artifacts prep: ~0.5 hr