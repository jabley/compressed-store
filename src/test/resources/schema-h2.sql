CREATE TABLE tomcat$sessions (
    id              varchar(40) not null PRIMARY KEY,
    valid           char(1) not null,
    maxinactive     int not null,
    lastaccess      bigint not null,
    app             varchar(255) not null,
    data            mediumblob not null
);

CREATE UNIQUE INDEX TOMCAT_SESSION_IX1 ON TOMCAT$SESSIONS (ID);
CREATE INDEX TOMCAT_SESION_IX2 ON TOMCAT$SESSIONS (APP);
