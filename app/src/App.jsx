import React from 'react';
import { HashRouter, Routes, Route, Navigate } from 'react-router-dom';
import { StoreProvider, useStore } from './store.jsx';
import { Splash, Welcome, MeetNeelu, Goal, Diet, Body, Target, Activity, MealTimes, Building, Plan, Notify } from './screens/onboarding.jsx';
import { Home, AddSheet } from './screens/Home.jsx';
import { Camera, Scanning, Result, Logged } from './screens/scan.jsx';
import { Search, FoodItem, MealDetail } from './screens/food.jsx';
import { Insights, Streak, Challenges, Weight } from './screens/progress.jsx';import { Inbox, Profile, Reminders } from './screens/settings.jsx';
import Snackbar from './components/Snackbar.jsx';

function Root() {
  const { profile } = useStore();
  if (!profile) return null;
  return <Navigate to={profile.onboarded ? '/home' : '/splash'} replace />;
}

export default function App() {
  return (
    <HashRouter>
      <StoreProvider>
        <Routes>
          <Route path="/" element={<Root />} />
          <Route path="/splash" element={<Splash />} />
          <Route path="/welcome" element={<Welcome />} />
          <Route path="/meet-neelu" element={<MeetNeelu />} />
          <Route path="/goal" element={<Goal />} />
          <Route path="/diet" element={<Diet />} />
          <Route path="/body" element={<Body />} />
          <Route path="/target" element={<Target />} />
          <Route path="/activity" element={<Activity />} />
          <Route path="/meal-times" element={<MealTimes />} />
          <Route path="/building" element={<Building />} />
          <Route path="/plan" element={<Plan />} />
          <Route path="/notify" element={<Notify />} />
          <Route path="/home" element={<Home />} />
          <Route path="/add" element={<AddSheet />} />
          <Route path="/camera" element={<Camera />} />
          <Route path="/scanning" element={<Scanning />} />
          <Route path="/result" element={<Result />} />
          <Route path="/logged" element={<Logged />} />
          <Route path="/meal/:id" element={<MealDetail />} />
          <Route path="/search" element={<Search />} />
          <Route path="/food" element={<FoodItem />} />
          <Route path="/insights" element={<Insights />} />
          <Route path="/streak" element={<Streak />} />
          <Route path="/challenges" element={<Challenges />} />
          <Route path="/weight" element={<Weight />} />
          <Route path="/inbox" element={<Inbox />} />
          <Route path="/profile" element={<Profile />} />
          <Route path="/reminders" element={<Reminders />} />
          <Route path="*" element={<Root />} />
        </Routes>
        <Snackbar />
      </StoreProvider>
    </HashRouter>
  );
}
