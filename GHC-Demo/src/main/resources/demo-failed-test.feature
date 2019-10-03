Feature: Login Service Positive Tests

  Scenario: create login


    Given url 'http://localhost:8080/ghc/login'
    And request {firstName: 'Mary', lastName: 'Doe', email: 'mary@zz.com', password: 'xyzAbc#12' }
    When method post
    Then status 200
    And match response == { userName: 'mary@zz.com' }


    * print '********Login response: ', response

