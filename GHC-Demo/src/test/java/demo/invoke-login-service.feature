Feature: Invoke Login Service

  Scenario: Register for GHC - successful


  Scenario: create login with duplicate email address

    * eval
  """
    if (karate.get('expectedHTTPStatusCode') == null) {
      karate.set('expectedHTTPStatusCode', 201);
    }
  """

    * def loginRequest = {firstName: 'Mary', lastName: 'Doe', email: 'mary@zz.com', password: 'xyzAbc#12' }

    * print '********Login request: ', loginRequest

    Given url demoBaseUrl
    And path 'ghc/login'
    And request loginRequest
    When method post
    Then assert responseStatus == expectedHTTPStatusCode

    * print '********Login response: ', response
