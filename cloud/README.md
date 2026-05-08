# Cloud Deployment Guide

This folder documents how the Event Registration System can be deployed to a cloud or Kubernetes environment. The application is already containerized with Docker and split into independent services, so the cloud deployment model keeps the same microservices architecture used in local development.

## Cloud Readiness

The system is cloud-ready because it has:

- separate deployable backend services
- an API Gateway as the public entry point
- Eureka service discovery for service-to-service communication
- Spring Cloud Config for centralized configuration
- PostgreSQL databases separated by service domain
- Dockerfiles for every backend service
- Docker Compose for local orchestration
- Kubernetes manifests for cloud-style orchestration

## Target Architecture

```text
User / Frontend
      |
      v
API Gateway Service :8080
      |
      +--> auth-service :8081
      +--> event-service :8082
      +--> registration-service :8083
      +--> notification-service :8084

Shared platform services:

- config-server :8888
- eureka-server :8761
- postgres :5432
```

## Deployment Options

### Option 1: Docker Compose on a Cloud VM

This is the simplest deployment option for a course project.

1. Create a VM on AWS EC2, Azure VM, Google Compute Engine, DigitalOcean, or any similar provider.
2. Install Docker and Docker Compose.
3. Clone the project repository.
4. Run:

```bash
docker compose up -d --build
```

5. Expose only the API Gateway port to users:

```text
8080
```

The internal services still run separately, but external clients should communicate through the gateway.

### Option 2: Kubernetes

The file `kubernetes/event-registration-system.yml` defines Kubernetes resources for the backend platform:

- ConfigMap for PostgreSQL database initialization
- PostgreSQL Deployment and Service
- Config Server Deployment and Service
- Eureka Server Deployment and Service
- API Gateway Deployment and Service
- Auth Service Deployment and Service
- Event Service Deployment and Service
- Registration Service Deployment and Service
- Notification Service Deployment and Service

Build the Docker images:

```bash
docker build -t event-registration/config-server:latest -f config-server/Dockerfile .
docker build -t event-registration/eureka-server:latest -f eureka-server/Dockerfile .
docker build -t event-registration/api-gateway:latest -f api-gateway/Dockerfile .
docker build -t event-registration/auth-service:latest -f auth-service/Dockerfile .
docker build -t event-registration/event-service:latest -f event-service/Dockerfile .
docker build -t event-registration/registration-service:latest -f registration-service/Dockerfile .
docker build -t event-registration/notification-service:latest -f notification-service/Dockerfile .
```

For a local Kubernetes cluster such as Docker Desktop or Minikube, apply the manifests:

```bash
kubectl apply -f cloud/kubernetes/event-registration-system.yml
```

Check the pods:

```bash
kubectl get pods
kubectl get services
```

For Minikube, expose the gateway:

```bash
minikube service api-gateway
```

For a real cloud Kubernetes cluster, replace the local image names with pushed registry images such as:

```text
docker.io/<username>/event-registration-api-gateway:latest
```

Then set the API Gateway service type to `LoadBalancer`.

## Why This Counts as Cloud

The project does not depend on one monolithic application server. Each service can be scaled, restarted, and deployed independently. Kubernetes or Docker Compose provides orchestration, while the API Gateway, Eureka Server, and Config Server provide common cloud-native microservices infrastructure.

## Production Notes

For a production cloud deployment, the following should be improved:

- move database passwords and JWT secrets to Kubernetes Secrets or a cloud secret manager
- use managed PostgreSQL instead of an in-cluster PostgreSQL pod
- push images to a container registry
- add readiness and liveness probes
- add centralized logging and monitoring
- configure HTTPS at the load balancer or ingress layer
