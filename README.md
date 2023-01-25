# Perpheads Files

A very basic [ShareX](https://getsharex.com/) target for uploading images/files that also
supports sharing files in a P2P fashion similar to [JustBeamIt](https://justbeamit.com/).

Perpheads Files is written entirely in Kotlin, using [Ktor](https://ktor.io/) as the
web server and Restful API, and [KotlinJS/ for React](https://kotlinlang.org/docs/js-get-started.html)
as a single page application.

[![build](https://github.com/Perpheads/ph-files/actions/workflows/build.yml/badge.svg)](https://github.com/Perpheads/ph-files/actions/workflows/build.yml)

## How to deploy

The application is built as a docker container.
You can find prebuilt containers in the docker registry
of this repository. 

The following environment variables can be set to configure the
settings of the docker container.

Please note that this application uses http rather than https and is expected
to be run behind a reverse proxy that does TLS termination (For example nginx).
By default, the application trusts any forwarded IP headers, so using it without
such a proxy will be dangerous.

The files are stored locally in the docker container (by default in the ``/app/files`` directory).
If you want to persist the uploaded files, create  a volume at that path.

To run the docker container, run the following command and add all of the required environment variables from below:
```bash
docker run \
-e FILES_DB_HOST=mysql \
-e ... \
ghcr.io/perpheads/ph-files:latest
```

You can of course also use the docker image with Kubernetes or Docker Compose.


### Environment Variables

#### Database Settings

| Environment Variable   | Default   | Description                              |
|------------------------|-----------|------------------------------------------|
| FILES_DB_HOST          | localhost | Host of the MySQL Database               |
| FILES_DB_PORT          | 3306      | Port of the MySQL Database               |
| FILES_DB_DATABASE      | ph_files  | Database name                            |
| FILES_DB_USER          | ph_files  | User to access the database              |
| FILES_DB_PASSWORD      |           | Password for the user                    |
| FILES_DB_MAX_POOL_SIZE | 10        | Maximum open connections to the database |

#### Cookie Settings

| Environment Variable | Default             | Description                                     |
|----------------------|---------------------|-------------------------------------------------|
| COOKIE_DOMAIN        | files.perpheads.com | The domain set on the authentication cookie     |
| COOKIE_SECURE        | true                | If the cookie may only be transmitted via HTTPS |


#### CORS Settings

| Environment Variable | Default             | Description                                                                |
|----------------------|---------------------|----------------------------------------------------------------------------|
| CORS_HOST            | files.perpheads.com | The host to be used for CORS requests                                      |
| CORS_ANY_HOST        | false               | Set this to true to essentially disable CORS (only do this in development) |

#### Other Settings

| Environment Variable | Default             | Description                                                |
|----------------------|---------------------|------------------------------------------------------------|
| FILES_FOLDER         | files               | Path to the folder where the uploaded files will be stored |
| CONTACT_EMAIL        | fredy@perpheads.com | The email shown on the contact page                        |
| PORT                 | 8080                | The port the application will run on                       |



## Development

### Requirements

- IntelliJ IDE or Gradle
- MySQL or MariaDB as a database
- JDK 17+

This project uses [Jooq](https://www.jooq.org/) in combination
with [Flyway](https://flywaydb.org/) to migrate an existing
database and automatically generate
a typesafe Java API for accessing the database.

This implies that for syntax highlighting to work properly in the
DAOs, you will have to first run the application successfully using gradle.


### Gradle configuration

You need two configurations to run the serer and the client. The client configuration has a continuous build feature that
can automatically detect changes and rebuilds the project similar to how node works.

#### Server
In IntelliJ, create a new gradle config with a ``:server:run`` task.
If you are using gradle on its own, please simply set the following environment variables
in your operating system's environment before invoking gradle.

Besides the database config (which might or might not already work with the default values),
the following environment variables have to be set in the gradle run config:

| Environment Variable | Value     |
|----------------------|-----------|
| DEVELOPMENT          | true      |
| COOKIE_DOMAIN        | localhost |
| COOKIE_SECURE        | false     |
| CORS_ANY_HOST        | true      |
| CORS_HOST            | localhost |

If everything is filled in correctly, the database should automatically be migrated upon running
the configuration. Jooq should generate all of its files and the server should start
and respond at the specified port (or 8080 as default).


### Client
In IntelliJ, create a new gradle config with a ``:client:browserDevelopmentRun --continuous`` task.
Ensure to run set the ```DEVELOPMENT``` environment variable to `true` here as well.

When running the client configuration, chrome will be opened automatically when the
compilation is finished, redirecting you to the correct host/port. 
The configuration expects the server to already be running on port `8080` of
the same hostname, while the development server is hardcoded to run on port `8081`.


### Running the application

After creating a gradle run configuration and selecting it as
the active configuration, simply hit the play button.
If you are using gradle on its own, simply run gradle run as discussed above.



### Creating an initial admin user

Unfortunately, there is no easy way to create the first admin user yet and
it has to be done manually through the database.
Open the ``users`` table in a program like HeidiSQL or DBeaver and
insert a new user.
The password for the new user has to be hashed using bcrypt 2a,
which can easily be done on [this website](https://www.browserling.com/tools/bcrypt).

### Use with ShareX

After logging in, press the three dots in the top right and select
```Get API Key```. From here you can copy the ShareX config
that can be used in ShareX's custom uploader settings.
Don't forget to also change the upload targets to
your custom uploader, after importing it.
