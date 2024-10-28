import Image from "next/image";
import { useLogOut } from "@/hooks";
import { useMutation } from "@tanstack/react-query";

interface MemberModalProps {
  image: string;
  nickname: string;
  email: string;
}

const MemberModal: React.FC<MemberModalProps> = ({
  image,
  nickname,
  email,
}) => {
  // TODO: 이미지, 닉네임, 이메일 주스탠드로 관리하는 로직

  const { mutate: handleLogout, isPending } = useLogOut();
  return (
    <div className="absolute right-0 z-50 mt-2 flex flex-col items-center justify-center gap-10 rounded-10 border border-gray-400 bg-indigo-800 p-20 text-white shadow-lg">
      <div className="flex flex-col items-center justify-center gap-10 p-10">
        <Image
          src={image ? `${image}` : "/profile.png"}
          alt={nickname}
          width={100}
          height={100}
          className="rounded-full"
        />
        <div className="whitespace-nowrap text-20 font-600">{nickname}</div>
        <div className="whitespace-nowrap text-16">{email}</div>
      </div>
      {/* 구분선 */}
      <div className="h-1 self-stretch border border-gray-400"></div>
      {/* 구분선 */}
      <div className="h-1 self-stretch border border-gray-400"></div>
      <button
        onClick={() => handleLogout()}
        className="rounded bg-red-500 px-4 py-2 text-white transition hover:bg-red-600 disabled:bg-gray-400"
        disabled={isPending}
      >
        {isPending ? "Logging out..." : "Logout"}
      </button>
    </div>
  );
};

export default MemberModal;