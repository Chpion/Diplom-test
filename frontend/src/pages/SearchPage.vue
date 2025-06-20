<template>
  <q-page padding>
    <q-card class="q-pa-md">
      <q-card-section>
        <div class="text-h6">Поиск</div>
      </q-card-section>
      <q-card-section>
        <q-form @submit="onSearch">
          <div class="row items-center">
            <div class="col">
              <q-input
                v-model="keyword"
                label="Введите адрес. Например, обл. Ульяновская, г. Ульяновск, пр-кт. Созидателей"
                :rules="[(val) => !!val || 'Keyword is required']"
                input-debounce="600"
                @keyup.enter="onSearch"
              />
            </div>
            <div class="col-auto q-ml-md">
              <q-btn flat color="primary" label="Расширенный поиск" @click="goToAdvancedSearch" />
            </div>
          </div>
          <q-btn type="submit" color="primary" label="Поиск" class="q-mt-md q-mr-sm" :loading="loading" />
          <q-btn color="secondary" label="Получить результат" class="q-mt-md" :loading="loadingDetails" @click="onGetDetails" />
        </q-form>
      </q-card-section>
      <q-card-section v-if="results?.length">
        <q-list bordered>
          <q-item v-for="(result, index) in results" :key="index" clickable @click="selectResult(result)">
            <q-item-section>{{ result }}</q-item-section>
          </q-item>
        </q-list>
      </q-card-section>
      <q-card-section v-if="addressInfo">
        <div class="text-subtitle1">Информация об адресе:</div>
        <q-list dense>
          <q-item>
            <q-item-section>
              <q-item-label>Название: {{ addressInfo.name }} {{ addressInfo.socr ? addressInfo.socr + '.' : '' }}</q-item-label>
              <q-item-label v-if="addressInfo.korp">Корпус: {{ addressInfo.korp }}</q-item-label>
              <q-item-label v-if="addressInfo.code">Код КЛАДР: {{ addressInfo.code }}</q-item-label>
              <q-item-label v-if="addressInfo.ocatd">Код ОКАТО: {{ addressInfo.ocatd }}</q-item-label>
              <q-item-label v-if="addressInfo.postalIndex">Почтовый индекс: {{ addressInfo.postalIndex }}</q-item-label>
              <q-item-label v-if="addressInfo.gninmb">ГНИМБ: {{ addressInfo.gninmb }}</q-item-label>
              <q-item-label v-if="addressInfo.uno">УНО: {{ addressInfo.uno }}</q-item-label>
              <q-item-label v-if="addressInfo.status !== null">Статус: {{ addressInfo.status }}</q-item-label>
            </q-item-section>
          </q-item>
        </q-list>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup>
import { ref } from 'vue';
import { Notify } from 'quasar';
import { useStore } from 'vuex';
import { useRouter } from 'vue-router';
import api from 'src/services/api';

const store = useStore();
const router = useRouter();
const keyword = ref('');
const results = ref([]);
const addressInfo = ref(null);
const loading = ref(false);
const loadingDetails = ref(false);

const onSearch = async () => {
  if (keyword.value.length < 3 || loading.value) {
    if (keyword.value.length < 3) {
      Notify.create({
        type: 'negative',
        message: 'Введите не менее 3 символов',
      });
    }
    return;
  }
  try {
    const response = await api.searchES(keyword.value);
    results.value = response.data || [];
    addressInfo.value = null; // Clear address info on search
  } catch (error) {
    Notify.create({
      type: 'negative',
      message: 'Поиск не удался: ' + (error.response?.data || 'Неизвестная ошибка'),
    });
    results.value = [];
  } finally {
    loading.value = false;
  }
};

const onGetDetails = async () => {
  if (keyword.value.length < 3 || loadingDetails.value) {
    if (keyword.value.length < 3) {
      Notify.create({
        type: 'negative',
        message: 'Введите не менее 3 символов',
      });
    }
    return;
  }
  if (!store.getters['auth/isAuthenticated']) {
    Notify.create({
      type: 'negative',
      message: 'Требуется авторизация',
    });
    router.push('/login');
    return;
  }
  loadingDetails.value = true;
  try {
    const encodedKeyword = encodeURIComponent(keyword.value);
    console.debug(`Sending getAddressInfo for keyword: ${encodedKeyword}`);
    const { data } = await store.dispatch('auth/getAddressInfo', encodedKeyword);
    addressInfo.value = data;
    results.value = [];
  } catch (error) {
    const message = error.response?.status === 400
      ? `Ошибка: ${error.response?.data?.error || 'Проверьте правильность введённого адреса'}`
      : `Не удалось получить информацию об адресе: ${error.response?.data?.error || error.message}`;
    Notify.create({
      type: 'negative',
      message,
    });
    addressInfo.value = null;
  } finally {
    loadingDetails.value = false;
  }
};

const selectResult = (result) => {
  const parts = keyword.value.split(', ').filter(p => p.trim());
  if (parts.length === 0) {
    keyword.value = result.trim() + ', ';
  } else {
    const lastPart = parts[parts.length - 1];
    const isKeyword = !lastPart.match(/^(обл\.|г\.|ул\.|пр-кт\.|пер\.|ш\.|пл\.|д\.|б-р\.)/i);
    if (isKeyword) {
      parts[parts.length - 1] = result.trim();
    } else {
      parts.push(result.trim());
    }
    keyword.value = parts.join(', ') + ', ';
  }
  results.value = [];
  addressInfo.value = null;
  onSearch();
};

const goToAdvancedSearch = () => {
  router.push('/advanced-search');
};
</script>
