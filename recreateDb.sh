sudo docker stop me
sudo docker rm me
sudo docker volume prune -af
sudo docker run --name me -e POSTGRES_DB=mesh -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -d -p 5438:5432 postgres:14.5-alpine
