Feature: Login Service Negative Tests

  Scenario: create login with duplicate email address

    * def loginRequest = {firstName: 'Mary', lastName: 'Doe', email: 'mary@xx.com', password: 'xyzAbc#12' }

    * print '********Login request: ', loginRequest
    Given url demoBaseUrl
    And path 'ghc/login'
    And request loginRequest
    When method post
    Then status 400
    And match response == { errorCode: 'login-svc-100', errorMessage: 'Login already exists with the given email' }

    * print '********Login response: ', response

  Scenario: create login with invalid password

    * def loginRequest = {firstName: 'Mary', lastName: 'Doe', email: 'jane@yy.com', password: 'yuhj' }

    * print '********Login request: ', loginRequest
      # add error code in the response
    Given url demoBaseUrl
    And path 'ghc/login'
    And request loginRequest
    When method post
    Then status 400
    And match response == { errorCode: 'login-svc-200', errorMessage: 'Password should be at least 8 characters or more.' }

    * print '********Login response: ', response

  Scenario: create login with invalid email

    * print '********Login request: ', loginRequest

    * def loginRequest = {firstName: 'Mary', lastName: 'Doe', email: 'jenny@zz.com.com', password: 'xyzAbc#12' }
    Given url demoBaseUrl
    And path 'ghc/login'
    And request loginRequest
    When method post
    Then status 400
    And match response == { errorCode: 'login-svc-300', errorMessage: 'Email address is invalid.' }

    * print '********Login response: ', response
