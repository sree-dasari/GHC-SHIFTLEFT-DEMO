Feature: Registration Service Tests

  Scenario: Register for GHC - successful

    * def registrationRequest = {userName: 'mary@xx.com', nameOnCard: 'Mary Doe', cardType: 'Visa', cardNumber: '1234 5678 9101 6789', cvv: '1234', expirationMonth:'10', expirationYear: '2021'}

    * call read('classpath:demo/invoke-login-service.feature')

    * print '********Registration request: ', registrationRequest

    Given url demoBaseUrl
    And path 'ghc/register'
    And request registrationRequest
    When method post
    Then status 200
    And match response == { confirmationNumber: '#uuid' }

    * print '********Registration response: ', response

