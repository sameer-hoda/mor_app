import { useStore } from '../store.jsx';

export default function Snackbar() {
  const { snack } = useStore();
  if (!snack) return null;
  return <div className={`snackbar ${snack.kind}`}>{snack.msg}</div>;
}
