import { PackageCheck } from 'lucide-react';

export function EmptyState({ title, text }: { title: string; text: string }) {
  return <div className="v-empty"><PackageCheck size={30} /><strong>{title}</strong><p>{text}</p></div>;
}

export function PageIntro({ eyebrow, title, text }: { eyebrow: string; title: string; text: string }) {
  return <div className="v-page-intro"><span className="v-eyebrow">{eyebrow}</span><h1>{title}</h1><p>{text}</p></div>;
}
