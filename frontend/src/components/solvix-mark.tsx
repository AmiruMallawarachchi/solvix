type SolvixMarkProps = {
  className?: string;
};

export function SolvixMark({ className }: SolvixMarkProps) {
  return (
    <svg
      aria-hidden="true"
      className={className}
      fill="none"
      viewBox="0 0 48 48"
      xmlns="http://www.w3.org/2000/svg"
    >
      <g
        stroke="currentColor"
        strokeLinecap="square"
        strokeWidth="2.6"
      >
        <path d="M24 4v10M31 6l-3.5 9M38 10l-7 7M43 18l-9 3M44 25l-10 .5M40 35l-9-5M33 41l-4-9M24 44V34M15 41l4-9M8 35l9-5M4 25l10 .5M5 18l9 3M10 10l7 7M17 6l3.5 9" />
      </g>
    </svg>
  );
}
