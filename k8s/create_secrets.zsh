set -ex

export SECRET_NAME=vedec-secret
export NS=vedec-prod

kubectl delete secret $SECRET_NAME --namespace=$NS --ignore-not-found

kubectl create secret generic \
  --namespace=$NS $SECRET_NAME \
  --from-literal=elastic_password=$ELASTICSEARCH_PASSWORD \
  --from-literal=hygraph_url=$HYGRAPH_URL