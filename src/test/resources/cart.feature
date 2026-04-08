@E2E
Feature: cart management
  Scenario: client adds an item and submits the cart
    Given a user named "Profesor Demo" with email prof@unibuc.ro exists
    And a component named "Placa Senzor" with id "comp999" exists
    When the client adds 3 units of "comp999" to the cart for "user123"
    Then the cart response status code is 200
    And the cart has 1 item with quantity 3
    When the client submits the cart for "user123"
    Then the cart response status code is 200
    And the cart status is "SUBMITTED"