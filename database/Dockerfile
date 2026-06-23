FROM mysql:8.0

# Set environment variables
ENV MYSQL_ROOT_PASSWORD=root
ENV MYSQL_DATABASE=buzzmeet
ENV MYSQL_USER=buzzmeet
ENV MYSQL_PASSWORD=buzzmeet123

# Copy initialization script
COPY Employee.sql /docker-entrypoint-initdb.d/

# Expose MySQL port
EXPOSE 3306

# Health check
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=5 \
  CMD mysqladmin ping -h localhost -u root -proot || exit 1
