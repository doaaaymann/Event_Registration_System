import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
});

let tokenGetter = null;

export function setTokenGetter(getter) {
  tokenGetter = getter;
}

api.interceptors.request.use((config) => {
  const token = tokenGetter?.();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const message =
      error?.response?.data?.message ||
      error?.response?.data?.error ||
      error?.message ||
      'Request failed';

    return Promise.reject(new Error(message));
  },
);

export default api;
